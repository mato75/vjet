/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *

 *******************************************************************************/
package org.eclipse.dltk.mod.core.search.indexing;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.mod.compiler.util.SimpleLookupTable;
import org.eclipse.dltk.mod.core.DLTKCore;
import org.eclipse.dltk.mod.core.DLTKLanguageManager;
import org.eclipse.dltk.mod.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.mod.core.IScriptProject;
import org.eclipse.dltk.mod.core.ISourceElementParser;
import org.eclipse.dltk.mod.core.ModelException;
import org.eclipse.dltk.mod.core.environment.EnvironmentManager;
import org.eclipse.dltk.mod.core.environment.EnvironmentPathUtils;
import org.eclipse.dltk.mod.core.environment.IEnvironment;
import org.eclipse.dltk.mod.core.environment.IFileHandle;
import org.eclipse.dltk.mod.core.search.SearchEngine;
import org.eclipse.dltk.mod.core.search.SearchParticipant;
import org.eclipse.dltk.mod.core.search.index.Index;
import org.eclipse.dltk.mod.internal.core.search.DLTKSearchDocument;
import org.eclipse.dltk.mod.internal.core.search.processing.JobManager;
import org.eclipse.dltk.mod.internal.core.util.Util;

class AddExternalFolderToIndex extends IndexRequest {
	IProject project;
	char[][] inclusionPatterns;
	char[][] exclusionPatterns;
	private IEnvironment environment;

	public AddExternalFolderToIndex(IPath folderPath, IProject project,
			char[][] inclusionPatterns, char[][] exclusionPatterns,
			IndexManager manager) {
		super(folderPath, manager);
		this.project = project;
		this.inclusionPatterns = inclusionPatterns;
		this.exclusionPatterns = exclusionPatterns;
	}

	public int hashCode() {
		if (this.containerPath != null) {
			return this.containerPath.hashCode();
		}
		return -1;
	}

	public boolean equals(Object o) {
		if (o instanceof AddExternalFolderToIndex) {
			if (this.containerPath != null) {
				return this.containerPath
						.equals(((AddExternalFolderToIndex) o).containerPath);
			}
		}
		return false;
	}

	private static String EXISTS = "OK"; //$NON-NLS-1$
	private static String DELETED = "DELETED"; //$NON-NLS-1$

	public boolean execute(IProgressMonitor progressMonitor) {
		if (this.isCancelled || progressMonitor != null
				&& progressMonitor.isCanceled()) {
			return true;
		}
		if (!this.project.isAccessible()) {
			return true; // nothing to do
		}
		IFileHandle folder = EnvironmentPathUtils.getFile(this.containerPath);
		if (folder == null || folder.isFile()) {
			return true; // nothing to do, source folder was removed
		}
		/* ensure no concurrent write access to index */
		Index index = this.manager
				.getIndex(this.containerPath, true, /*
													 * reuse index file
													 */false /*
															 * create if none
															 */);
		if (index != null) {
			if (JobManager.VERBOSE) {
				Util
						.verbose("-> no indexing required (index already exists) for " + this.containerPath); //$NON-NLS-1$
			}
			return true;
		}
		index = this.manager
				.getIndexForUpdate(this.containerPath, true, /*
															 * reuse index file
															 */true /*
																	 * create if
																	 * none
																	 */);
		if (index == null) {
			if (JobManager.VERBOSE) {
				Util
						.verbose("-> index could not be created for " + this.containerPath); //$NON-NLS-1$
			}
			return true;
		}
		ReadWriteMonitor monitor = index.monitor;
		if (monitor == null) {
			if (JobManager.VERBOSE) {
				Util
						.verbose("-> index for " + this.containerPath + " just got deleted"); //$NON-NLS-1$//$NON-NLS-2$
			}
			return true; // index got deleted since acquired
		}
		try {
			monitor.enterRead(); // ask permission to read
			final IPath container = this.containerPath;
			final IndexManager indexManager = this.manager;
			final IScriptProject project = DLTKCore.create(this.project);
			final ISourceElementParser parser = indexManager
					.getSourceElementParser(project);
			final SourceIndexerRequestor requestor = indexManager
					.getSourceRequestor(project);
			if (JobManager.VERBOSE) {
				Util.verbose("-> indexing " + this.containerPath.toString()); //$NON-NLS-1$
			}
			long initialTime = System.currentTimeMillis();
			String[] paths = index.queryDocumentNames(""); // all file names //$NON-NLS-1$
			if (paths != null) {
				int max = paths.length;
				/*
				 * check integrity of the existing index file if the length is
				 * equal to 0, we want to index the whole zip again If not, then
				 * we want to check that there is no missing entry, if one entry
				 * is missing then we recreate the index
				 */
				SimpleLookupTable indexedFileNames = new SimpleLookupTable(
						max == 0 ? 33 : max + 11);
				for (int i = 0; i < max; i++) {
					indexedFileNames.put(paths[i], DELETED);
				}
				this.visit(indexedFileNames, project, folder, parser,
						requestor, indexManager, container, false, null, index);
				boolean needToReindex = indexedFileNames.elementSize != max; // a
				// new
				// file
				// was
				// added
				if (!needToReindex) {
					Object[] valueTable = indexedFileNames.valueTable;
					for (int i = 0, l = valueTable.length; i < l; i++) {
						if (valueTable[i] == DELETED) {
							needToReindex = true; // a file was deleted so
							// re-index
							break;
						}
					}
					if (!needToReindex) {
						if (JobManager.VERBOSE) {
							Util
									.verbose("-> no indexing required (index is consistent with library) for " //$NON-NLS-1$
											+ this.containerPath.toString()
											+ " (" //$NON-NLS-1$
											+ (System.currentTimeMillis() - initialTime)
											+ "ms)"); //$NON-NLS-1$
						}
						this.manager.saveIndex(index); // to ensure its placed
						// into the saved state
						return true;
					}
				}
			}
			// Index the zip for the first time or reindex the zip in case the
			// previous index file has been corrupted
			// index already existed: recreate it so that we forget about
			// previous entries
			SearchParticipant participant = SearchEngine
					.getDefaultSearchParticipant();
			index = this.manager.recreateIndex(this.containerPath);
			if (index == null) {
				// failed to recreate index, see 73330
				this.manager.removeIndex(this.containerPath);
				return false;
			}
			this.visit(null, project, folder, parser, requestor, indexManager,
					container, true, participant, index);
			this.manager.saveIndex(index);
			if (JobManager.VERBOSE) {
				Util.verbose("-> done indexing of " //$NON-NLS-1$
						+ this.containerPath.toString() + " (" //$NON-NLS-1$
						+ (System.currentTimeMillis() - initialTime) + "ms)"); //$NON-NLS-1$
			}
		} catch (IOException ex) {
			if (JobManager.VERBOSE) {
				Util
						.verbose("-> failed to index " + this.containerPath + " because of the following exception:"); //$NON-NLS-1$ //$NON-NLS-2$
				ex.printStackTrace();
			}
			this.manager.removeIndex(this.containerPath);
			return false;
		} finally {
			monitor.exitRead(); // free read lock
		}
		return true;
	}

	private void visit(SimpleLookupTable table, IScriptProject project,
			IFileHandle folder, ISourceElementParser parser,
			SourceIndexerRequestor requestor, IndexManager indexManager,
			IPath container, boolean operation, SearchParticipant participant,
			Index index) {

		IDLTKLanguageToolkit toolkit = null;
		toolkit = DLTKLanguageManager.getLanguageToolkit(project);
		IFileHandle[] files = folder.getChildren();
		if (files != null) {
			for (int i = 0; i < files.length; ++i) {
				if (this.isCancelled) {
					if (JobManager.VERBOSE) {
						Util
								.verbose("-> indexing of " + this.containerPath.toString() + " has been cancelled"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					return;
				}
				if (files[i].isDirectory()) {
					IPath fPath = files[i].getFullPath();
					boolean valid = Util.isValidSourcePackageName(project,
							fPath);
					if (!((fPath.segmentCount() == 0 || valid))) {
						continue;
					}
					if (!Util.isExcluded(fPath, this.inclusionPatterns,
							this.exclusionPatterns, true)) {
						this.visit(table, project, files[i], parser, requestor,
								indexManager, container, operation,
								participant, index);
					}
				} else {
					String path = files[i].toOSString();
					IPath rPath = new Path(path);
					if (Util.isValidSourceModuleName(project, path)) {
						if (DLTKCore.DEBUG) {
							System.err.println("Out:" + path); //$NON-NLS-1$
						}
						if (this.exclusionPatterns == null
								&& this.inclusionPatterns == null) {
							if (!operation) {
								table.put(rPath.toString(), EXISTS);
							} else {
								this.indexDocument(parser, requestor,
										participant, index, path, toolkit);
							}
						} else {
							if (!Util.isExcluded(rPath, this.inclusionPatterns,
									this.exclusionPatterns, false)) {
								if (!operation) {
									table.put(rPath.toString(), EXISTS);
								} else {
									this.indexDocument(parser, requestor,
											participant, index, path, toolkit);
								}
							}
						}
						// indexManager.addExternalSource(rPath, container,
						// parser);
					}
				}
			}
		}
	}

	private void indexDocument(ISourceElementParser parser,
			SourceIndexerRequestor requestor, SearchParticipant participant,
			Index index, String path, IDLTKLanguageToolkit toolkit) {
		final IFileHandle ffile = getFile(path);
		if (ffile == null) {
			return;
		}
		final char[] contents;
		try {
			contents = Util.getResourceContentsAsCharArray(ffile);
		} catch (ModelException e) {
			if (DLTKCore.DEBUG) {
				e.printStackTrace();
			}
			return;
		}
		IPath dpath = new Path(path).removeFirstSegments(
				containerPath.segmentCount()).setDevice(null);
		DLTKSearchDocument entryDocument = new DLTKSearchDocument(dpath
				.toString(), this.containerPath, contents, participant, true,
				project);
		entryDocument.fullPath = EnvironmentPathUtils.getLocalPath(ffile
				.getFullPath());
		entryDocument.parser = parser;
		entryDocument.requestor = requestor;
		entryDocument.toolkit = toolkit;
		this.manager.indexDocument(entryDocument, participant, index,
				this.containerPath);
	}

	private IFileHandle getFile(String path) {
		if (environment == null) {
			environment = EnvironmentManager.getEnvironment(DLTKCore
					.create(project));
			if (environment == null) {
				return null;
			}
		}
		return environment.getFile(new Path(path));
	}

	public String toString() {
		return "adding " + this.containerPath + " to index " + this.containerPath; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
