/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.mod.core.search.indexing;

import org.eclipse.dltk.mod.core.DLTKCore;
import org.eclipse.dltk.mod.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.mod.core.ISourceElementParser;
import org.eclipse.dltk.mod.core.search.index.Index;

/**
 * Internal search document implementation
 */
public class InternalSearchDocument {
	protected Index index;
	protected String containerRelativePath;
	protected ISourceElementParser parser;
	public SourceIndexerRequestor requestor;
	public IDLTKLanguageToolkit toolkit;

	/*
	 * Hidden by API SearchDocument subclass
	 */
	public void addIndexEntry(char[] category, char[] key) {
		if (this.index != null) {
			index.addIndexEntry(category, key, getContainerRelativePath());
			if (category == IIndexConstants.TYPE_DECL && key != null) {
				int length = key.length;
				if (length > 1 && key[length - 2] == IIndexConstants.SEPARATOR
						&& key[length - 1] == IIndexConstants.SECONDARY_SUFFIX) {
					// This is a key of a secondary type => resetscriptmodel
					// manager secondary types cache for document path project
					// ModelManager manager = ModelManager.getModelManager();
					// manager.secondaryTypeAdding(getPath(), key);
					if (DLTKCore.DEBUG) {
						System.err
								.println("TODO: Scondary type index. Do we need it?"); //$NON-NLS-1$
					}
				}
			}
		}
	}

	public String getContainerRelativePath() {
		if (this.containerRelativePath == null)
			this.containerRelativePath = this.index
					.containerRelativePath(getPath());
		return this.containerRelativePath;
	}

	/*
	 * Hidden by API SearchDocument subclass
	 */
	public void removeAllIndexEntries() {
		if (this.index != null)
			index.remove(getContainerRelativePath());
	}

	/*
	 * Hidden by API SearchDocument subclass
	 */
	public String getPath() {
		return null; // implemented by subclass
	}

	public IDLTKLanguageToolkit getToolkit() {
		return this.toolkit;
	}

	public void setIndex(Index index) {
		this.index = index;
	}

	public Index getIndex() {
		return index;
	}

	public void setContainerRelativePath(String path) {
		this.containerRelativePath = path;
	}
}
