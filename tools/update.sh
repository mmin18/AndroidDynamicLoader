BASEDIR=$(dirname $0)
java -cp ${BASEDIR}/lib/anttasks.jar com.dianping.ant.UpdateProject ${BASEDIR} $*
