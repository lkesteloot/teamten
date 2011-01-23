@grep -v '^#' < %1 > %1.java
@REM don't use the real CLASSPATH, it affects ant.
@set MY_CLASSPATH=../dist/teamten.jar;../lib/*;.
@ant -f ../build.xml && javac -cp %MY_CLASSPATH% %1.java && java -cp %MY_CLASSPATH% -Xmx1000m %1 %2
