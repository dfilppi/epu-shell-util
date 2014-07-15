#This script provides the command and control utility for the
#GigaSpaces Technologies Inc. Service Grid

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
$DIR/../tools/groovy/bin/groovy $DIR/gs.groovy $*

if [ $? = 99 ]; then
  exit 0
fi

if [ $? != 0 ];then
  exit $?
fi

#got this far, just call regular
$DIR/gs.sh $*

exit $?

