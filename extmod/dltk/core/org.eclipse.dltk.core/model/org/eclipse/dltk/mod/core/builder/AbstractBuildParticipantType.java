/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.mod.core.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.mod.core.IScriptProject;

public abstract class AbstractBuildParticipantType implements
		IBuildParticipantFactory {

	/**
	 * @deprecated
	 */
	protected final void getID() {
	}

	/**
	 * @deprecated
	 */
	protected final void getName() {
	}

	/**
	 * @deprecated
	 */
	protected final void getNature() {
	}

	public abstract IBuildParticipant createBuildParticipant(
			IScriptProject project) throws CoreException;

}
