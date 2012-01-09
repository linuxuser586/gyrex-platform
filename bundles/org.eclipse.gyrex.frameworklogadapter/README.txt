Gyrex FrameworkLog Adapter
--------------------------


Make sure that this bundle is placed in the same folder as "org.eclipse.osgi".


You also need to set the following properties to activate the FrameworkLog.

  osgi.framework.extensions=org.eclipse.gyrex.log.frameworklogadapter

This can be done in the launch configuration via '-D..." VM arguments or via direct config.ini entries.
Usually, p2 should add it automatically to config.ini on product builds.
