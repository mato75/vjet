package org.eclipse.dltk.mod.javascript.internal.launching;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.dltk.mod.debug.core.DLTKDebugPlugin;
import org.eclipse.dltk.mod.debug.core.IDbgpService;
import org.eclipse.dltk.mod.debug.core.model.IScriptDebugTarget;
import org.eclipse.dltk.mod.debug.ui.ScriptStreamProxy;
import org.eclipse.dltk.mod.internal.debug.core.model.ScriptDebugTarget;
import org.eclipse.dltk.mod.internal.launching.JavaScriptSourceLookupDirector;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.ui.console.IOConsole;

public class JavaLocalApplicationLaunchConfigurationDelegate extends
		JavaLaunchDelegate implements ILaunchConfigurationDelegate {

	public String getVMArguments(ILaunchConfiguration configuration)
			throws CoreException {
		return super.getVMArguments(configuration)
				+ " -javaagent:C:/rhino-agent.jar";
	}

	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
//		System.out.println("launching");
		IDbgpService dbgpService = null;
		try {
			dbgpService = DLTKDebugPlugin.getDefault().getDbgpService();
			
			IScriptDebugTarget target = new ScriptDebugTarget("org.eclipse.dltk.mod.debug.javascriptModel", dbgpService,
					"hello", launch,null);
			IOConsole cs=new IOConsole("aa",null);
			ScriptStreamProxy proxy = new ScriptStreamProxy(cs);
			target.setStreamProxy(proxy);
			launch.addDebugTarget(target);			
			final ISourceLocator sourceLocator = launch.getSourceLocator();
			final JavaScriptSourceLookupDirector l=new JavaScriptSourceLookupDirector();
			launch.setSourceLocator(new ISourceLocator(){

				public Object getSourceElement(IStackFrame stackFrame) {
					Object sourceElement = sourceLocator.getSourceElement(stackFrame);
					if (sourceElement!=null) return sourceElement;
					return l.getSourceElement(stackFrame);
				}
				
			});
		} catch (Exception e) {

		}

		super.launch(configuration, mode, launch, monitor);
	}

	public static final String LOCAL_APPLICATION = "debug.localJavaApplication";

}
