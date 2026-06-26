@rem Gradle startup script for Windows
@set JAVA_OPTS=%JAVA_OPTS% -Xmx4096m
@java %JAVA_OPTS% -classpath %~dp0\gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain %*
