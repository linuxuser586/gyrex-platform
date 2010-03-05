Gyrex FrameworkLog Adapter
--------------------------


Make sure that this bundle is placed in the same folder as "org.eclipse.osgi".


You also need to set the following properties to activate the FrameworkLog.

  osgi.framework.extensions=org.eclipse.gyrex.log.frameworklogadapter
  osgi.hook.configurators.exclude=org.eclipse.core.runtime.internal.adaptor.EclipseLogHook

This can be done in the launch configuration via '-D..." VM arguments or via direct config.ini entries.
