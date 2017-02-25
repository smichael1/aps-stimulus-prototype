@REM vslice launcher script
@REM
@REM Environment:
@REM JAVA_HOME - location of a JDK home dir (optional if java on path)
@REM CFG_OPTS  - JVM options (optional)
@REM Configuration:
@REM VSLICE_config.txt found in the VSLICE_HOME.
@setlocal enabledelayedexpansion

@echo off

if "%VSLICE_HOME%"=="" set "VSLICE_HOME=%~dp0\\.."

set "APP_LIB_DIR=%VSLICE_HOME%\lib\"

rem Detect if we were double clicked, although theoretically A user could
rem manually run cmd /c
for %%x in (!cmdcmdline!) do if %%~x==/c set DOUBLECLICKED=1

rem FIRST we load the config file of extra options.
set "CFG_FILE=%VSLICE_HOME%\VSLICE_config.txt"
set CFG_OPTS=
if exist %CFG_FILE% (
  FOR /F "tokens=* eol=# usebackq delims=" %%i IN ("%CFG_FILE%") DO (
    set DO_NOT_REUSE_ME=%%i
    rem ZOMG (Part #2) WE use !! here to delay the expansion of
    rem CFG_OPTS, otherwise it remains "" for this loop.
    set CFG_OPTS=!CFG_OPTS! !DO_NOT_REUSE_ME!
  )
)

rem We use the value of the JAVACMD environment variable if defined
set _JAVACMD=%JAVACMD%

if "%_JAVACMD%"=="" (
  if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
  )
)

if "%_JAVACMD%"=="" set _JAVACMD=java

rem Detect if this java is ok to use.
for /F %%j in ('"%_JAVACMD%" -version  2^>^&1') do (
  if %%~j==java set JAVAINSTALLED=1
  if %%~j==openjdk set JAVAINSTALLED=1
)

rem BAT has no logical or, so we do it OLD SCHOOL! Oppan Redmond Style
set JAVAOK=true
if not defined JAVAINSTALLED set JAVAOK=false

if "%JAVAOK%"=="false" (
  echo.
  echo A Java JDK is not installed or can't be found.
  if not "%JAVA_HOME%"=="" (
    echo JAVA_HOME = "%JAVA_HOME%"
  )
  echo.
  echo Please go to
  echo   http://www.oracle.com/technetwork/java/javase/downloads/index.html
  echo and download a valid Java JDK and install before running vslice.
  echo.
  echo If you think this message is in error, please check
  echo your environment variables to see if "java.exe" and "javac.exe" are
  echo available via JAVA_HOME or PATH.
  echo.
  if defined DOUBLECLICKED pause
  exit /B 1
)


rem We use the value of the JAVA_OPTS environment variable if defined, rather than the config.
set _JAVA_OPTS=%JAVA_OPTS%
if "!_JAVA_OPTS!"=="" set _JAVA_OPTS=!CFG_OPTS!

rem We keep in _JAVA_PARAMS all -J-prefixed and -D-prefixed arguments
rem "-J" is stripped, "-D" is left as is, and everything is appended to JAVA_OPTS
set _JAVA_PARAMS=
set _APP_ARGS=

:param_loop
call set _PARAM1=%%1
set "_TEST_PARAM=%~1"

if ["!_PARAM1!"]==[""] goto param_afterloop


rem ignore arguments that do not start with '-'
if "%_TEST_PARAM:~0,1%"=="-" goto param_java_check
set _APP_ARGS=!_APP_ARGS! !_PARAM1!
shift
goto param_loop

:param_java_check
if "!_TEST_PARAM:~0,2!"=="-J" (
  rem strip -J prefix
  set _JAVA_PARAMS=!_JAVA_PARAMS! !_TEST_PARAM:~2!
  shift
  goto param_loop
)

if "!_TEST_PARAM:~0,2!"=="-D" (
  rem test if this was double-quoted property "-Dprop=42"
  for /F "delims== tokens=1,*" %%G in ("!_TEST_PARAM!") DO (
    if not ["%%H"] == [""] (
      set _JAVA_PARAMS=!_JAVA_PARAMS! !_PARAM1!
    ) else if [%2] neq [] (
      rem it was a normal property: -Dprop=42 or -Drop="42"
      call set _PARAM1=%%1=%%2
      set _JAVA_PARAMS=!_JAVA_PARAMS! !_PARAM1!
      shift
    )
  )
) else (
  if "!_TEST_PARAM!"=="-main" (
    call set CUSTOM_MAIN_CLASS=%%2
    shift
  ) else (
    set _APP_ARGS=!_APP_ARGS! !_PARAM1!
  )
)
shift
goto param_loop
:param_afterloop

set _JAVA_OPTS=!_JAVA_OPTS! !_JAVA_PARAMS!
:run
 
set "APP_CLASSPATH=%APP_LIB_DIR%\org.tmt.vslice-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.scala-lang.scala-library-2.11.8.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-actor_2.11-2.4.11.jar;%APP_LIB_DIR%\com.typesafe.config-1.3.0.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-java8-compat_2.11-0.7.0.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-remote_2.11-2.4.11.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-stream_2.11-2.4.11.jar;%APP_LIB_DIR%\com.typesafe.ssl-config-akka_2.11-0.2.1.jar;%APP_LIB_DIR%\com.typesafe.ssl-config-core_2.11-0.2.1.jar;%APP_LIB_DIR%\org.scala-lang.modules.scala-parser-combinators_2.11-1.0.4.jar;%APP_LIB_DIR%\org.reactivestreams.reactive-streams-1.0.0.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-protobuf_2.11-2.4.11.jar;%APP_LIB_DIR%\io.netty.netty-3.10.6.Final.jar;%APP_LIB_DIR%\org.uncommons.maths.uncommons-maths-1.2.2a.jar;%APP_LIB_DIR%\io.aeron.aeron-driver-1.0.1.jar;%APP_LIB_DIR%\io.aeron.aeron-client-1.0.1.jar;%APP_LIB_DIR%\org.agrona.Agrona-0.5.4.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-http-experimental_2.11-2.4.11.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-http-core_2.11-2.4.11.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-parsing_2.11-2.4.11.jar;%APP_LIB_DIR%\org.tmt.pkg_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.tmt.log_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-slf4j_2.11-2.4.11.jar;%APP_LIB_DIR%\org.slf4j.slf4j-api-1.7.16.jar;%APP_LIB_DIR%\com.typesafe.scala-logging.scala-logging-slf4j_2.11-2.1.2.jar;%APP_LIB_DIR%\com.typesafe.scala-logging.scala-logging-api_2.11-2.1.2.jar;%APP_LIB_DIR%\ch.qos.logback.logback-classic-1.1.1.jar;%APP_LIB_DIR%\ch.qos.logback.logback-core-1.1.1.jar;%APP_LIB_DIR%\org.codehaus.janino.janino-2.7.6.jar;%APP_LIB_DIR%\org.codehaus.janino.commons-compiler-2.7.6.jar;%APP_LIB_DIR%\net.logstash.logback.logstash-logback-encoder-3.1.jar;%APP_LIB_DIR%\org.tmt.loc_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.jmdns.jmdns-3.5.1.jar;%APP_LIB_DIR%\org.tmt.ccs_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.tmt.events_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.tmt.util_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\com.typesafe.akka.akka-http-spray-json-experimental_2.11-2.4.11.jar;%APP_LIB_DIR%\io.spray.spray-json_2.11-1.3.2.jar;%APP_LIB_DIR%\org.scala-lang.scala-reflect-2.11.8.jar;%APP_LIB_DIR%\org.tmt.tracklocation_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.tmt.cs_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.tmt.configserviceannex_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.eclipse.jgit.org.eclipse.jgit-3.5.1.201410131835-r.jar;%APP_LIB_DIR%\com.jcraft.jsch-0.1.50.jar;%APP_LIB_DIR%\com.googlecode.javaewah.JavaEWAH-0.7.9.jar;%APP_LIB_DIR%\org.apache.httpcomponents.httpclient-4.1.3.jar;%APP_LIB_DIR%\org.apache.httpcomponents.httpcore-4.1.4.jar;%APP_LIB_DIR%\commons-logging.commons-logging-1.1.1.jar;%APP_LIB_DIR%\commons-codec.commons-codec-1.4.jar;%APP_LIB_DIR%\org.tmatesoft.svnkit.svnkit-1.8.11.jar;%APP_LIB_DIR%\com.jcraft.jsch.agentproxy.svnkit-trilead-ssh2-0.0.7.jar;%APP_LIB_DIR%\com.trilead.trilead-ssh2-1.0.0-build220.jar;%APP_LIB_DIR%\com.jcraft.jsch.agentproxy.core-0.0.7.jar;%APP_LIB_DIR%\net.java.dev.jna.jna-platform-4.1.0.jar;%APP_LIB_DIR%\net.java.dev.jna.jna-4.1.0.jar;%APP_LIB_DIR%\com.jcraft.jsch.agentproxy.connector-factory-0.0.7.jar;%APP_LIB_DIR%\com.jcraft.jsch.agentproxy.usocket-jna-0.0.7.jar;%APP_LIB_DIR%\net.java.dev.jna.platform-3.4.0.jar;%APP_LIB_DIR%\com.jcraft.jsch.agentproxy.usocket-nc-0.0.7.jar;%APP_LIB_DIR%\com.jcraft.jsch.agentproxy.sshagent-0.0.7.jar;%APP_LIB_DIR%\com.jcraft.jsch.agentproxy.pageant-0.0.7.jar;%APP_LIB_DIR%\de.regnis.q.sequence.sequence-library-1.0.3.jar;%APP_LIB_DIR%\org.tmatesoft.sqljet.sqljet-1.1.10.jar;%APP_LIB_DIR%\org.antlr.antlr-runtime-3.4.jar;%APP_LIB_DIR%\com.github.scopt.scopt_2.11-3.3.0.jar;%APP_LIB_DIR%\com.github.etaty.rediscala_2.11-1.6.0.jar;%APP_LIB_DIR%\org.scala-stm.scala-stm_2.11-0.7.jar;%APP_LIB_DIR%\de.heikoseeberger.akka-sse_2.11-1.5.0.jar;%APP_LIB_DIR%\org.tmt.ts_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.tmt.alarms_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\com.github.fge.json-schema-validator-2.2.6.jar;%APP_LIB_DIR%\com.google.code.findbugs.jsr305-3.0.0.jar;%APP_LIB_DIR%\joda-time.joda-time-2.3.jar;%APP_LIB_DIR%\com.googlecode.libphonenumber.libphonenumber-6.2.jar;%APP_LIB_DIR%\com.github.fge.json-schema-core-1.2.5.jar;%APP_LIB_DIR%\com.github.fge.uri-template-0.9.jar;%APP_LIB_DIR%\com.github.fge.msg-simple-1.1.jar;%APP_LIB_DIR%\com.github.fge.btf-1.2.jar;%APP_LIB_DIR%\com.google.guava.guava-16.0.1.jar;%APP_LIB_DIR%\com.github.fge.jackson-coreutils-1.8.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-databind-2.2.3.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-annotations-2.2.3.jar;%APP_LIB_DIR%\com.fasterxml.jackson.core.jackson-core-2.2.3.jar;%APP_LIB_DIR%\org.mozilla.rhino-1.7R4.jar;%APP_LIB_DIR%\javax.mail.mailapi-1.4.3.jar;%APP_LIB_DIR%\javax.activation.activation-1.1.jar;%APP_LIB_DIR%\net.sf.jopt-simple.jopt-simple-4.6.jar;%APP_LIB_DIR%\com.iheart.ficus_2.11-1.2.3.jar;%APP_LIB_DIR%\org.tmt.containercmd_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.tmt.seqsupport_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.tmt.support_2.11-0.3-SNAPSHOT.jar;%APP_LIB_DIR%\org.tmt.hcdexample_2.11-0.3-SNAPSHOT.jar"
set "APP_MAIN_CLASS=csw.examples.vslice.shared.TromboneApp"

if defined CUSTOM_MAIN_CLASS (
    set MAIN_CLASS=!CUSTOM_MAIN_CLASS!
) else (
    set MAIN_CLASS=!APP_MAIN_CLASS!
)

rem Call the application and pass all arguments unchanged.
"%_JAVACMD%" !_JAVA_OPTS! !VSLICE_OPTS! -cp "%APP_CLASSPATH%" %MAIN_CLASS% !_APP_ARGS!

@endlocal


:end

exit /B %ERRORLEVEL%
