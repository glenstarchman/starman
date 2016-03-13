ROOT_DIR="."
CLASS_PATH="$(echo $ROOT_DIR/lib/*.jar | tr ' ' ':'):$ROOT_DIR/config"

java -cp $CLASS_PATH starman.common.Deploy mandrill_publish project/invite project_invite
java -cp $CLASS_PATH starman.common.Deploy mandrill_publish project/contributor_accept project_contributor_accept
java -cp $CLASS_PATH starman.common.Deploy mandrill_publish project/contributor_request project_contributor_request
 

