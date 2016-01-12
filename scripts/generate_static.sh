ROOT_DIR="."
CLASS_PATH="$(echo $ROOT_DIR/lib/*.jar | tr ' ' ':'):$ROOT_DIR/config"

java -cp $CLASS_PATH com.didd.common.StaticGenerator
