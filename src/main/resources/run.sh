#!/bin/sh
quietGuestAndPlay() {
    export IF_DOWNLOAD=$1
    export DOWNLOAD_LOCATION=$2
    export CLIENT_NUM=$3
    export SERVER_ADDRESS=$4
    printEnv
    export COMMD="-e IF_DOWNLOAD=${IF_DOWNLOAD} \
    -e DOWNLOAD_LOCATION=${DOWNLOAD_LOCATION} \
    -e CLIENT_NUM=${CLIENT_NUM} \
    -e SERVER_ADDRESS=${SERVER_ADDRESS}  "	
    runDocker		  
}

quietThird() {
    export IF_DOWNLOAD=$1
    export DOWNLOAD_LOCATION=$2
    export CLIENT_NUM=$3
    export THIRD_HOST=$4
    export THIRD_PORT=$5
    export THIRD_CHANNEL_ID=$6
    export RSA_PUB=`cat docker/pub`
    export RSA_PRI=`cat docker/pri`
    printEnv
	
	export COMMD="-e IF_DOWNLOAD=${IF_DOWNLOAD} \
	-e DOWNLOAD_LOCATION=${DOWNLOAD_LOCATION} \
	-e CLIENT_NUM=${CLIENT_NUM} \
	-e THIRD_HOST=${THIRD_HOST} \
	-e THIRD_PORT=${THIRD_PORT} \
	-e THIRD_CHANNEL_ID=${THIRD_CHANNEL_ID} \
	-e RSA_PUB=${RSA_PUB} \
	-e RSA_PRI=${RSA_PRI}  "	
	runDocker  
}

quietSimu(){
    export SERVER_ADDRESS=$1
    export BET_COUNT_LIMIT=$2
	export ROBOT_COUNT=$3
    printEnv	
	export COMMD="-e SERVER_ADDRESS=${SERVER_ADDRESS} \
	-e BET_COUNT_LIMIT=${BET_COUNT_LIMIT} \
    -e ROBOT_COUNT=${ROBOT_COUNT}	"	
	runDocker  		
}

quietBack(){
    export MONGO_HOSTS=$1
    export MONGO_DATABASE=$2
  	export ROOM_NAME=$3
  	export EXCEL_LOCATION=$4
  	export OUTPUT_ROWS=$5
    printEnv	
	export COMMD="-e MONGO_HOSTS=${MONGO_HOSTS} \
	-e MONGO_DATABASE=${MONGO_DATABASE} \
	-e ROOM_NAME=${ROOM_NAME} \
	-e EXCEL_LOCATION=${EXCEL_LOCATION} \
	-e OUTPUT_ROWS=${OUTPUT_ROWS}  "	
	runDocker  	
}

interGuestAndPlay() {
  read -p "download? (y/n) " download
  if [ "$download" = "y" ]; then
    export IF_DOWNLOAD="true"
  else
    export IF_DOWNLOAD="false"
  fi
  read -p "download location : " download_location
  export DOWNLOAD_LOCATION=$download_location
  read -p "client number : " client_num
  export CLIENT_NUM=$client_num
  read -p "server address : " server_address
  export SERVER_ADDRESS=$server_address
  printEnv  
  export COMMD=" -e IF_DOWNLOAD=${IF_DOWNLOAD} \
  -e DOWNLOAD_LOCATION=${DOWNLOAD_LOCATION} \
  -e CLIENT_NUM=${CLIENT_NUM} \
  -e SERVER_ADDRESS=${SERVER_ADDRESS}  "	
  runDocker  	  
}

interThird() {
  read -p "download? (y/n) " download
  if [ "$download" = "y" ]; then
    export IF_DOWNLOAD="true"
  else
    export IF_DOWNLOAD="false"
  fi
  read -p "download location : " download_location
  export DOWNLOAD_LOCATION=$download_location
  read -p "client number : " client_num
  export CLIENT_NUM=$client_num
  read -p "third host : " third_host
  export THIRD_HOST=$third_host
  read -p "third port : " third_port
  export THIRD_PORT=$third_port
  read -p "third channel id : " third_channel_id
  export THIRD_CHANNEL_ID=$third_channel_id
  export RSA_PUB=`cat docker/pub`
  export RSA_PRI=`cat docker/pri`
  printEnv  
  export COMMD=" -e IF_DOWNLOAD=${IF_DOWNLOAD} \
  -e DOWNLOAD_LOCATION=${DOWNLOAD_LOCATION} \
  -e CLIENT_NUM=${CLIENT_NUM} \
  -e THIRD_HOST=${THIRD_HOST} \
  -e THIRD_PORT=${THIRD_PORT} \
  -e THIRD_CHANNEL_ID=${THIRD_CHANNEL_ID} \
  -e RSA_PUB=${RSA_PUB} \
  -e RSA_PRI=${RSA_PRI}  "	
  runDocker    
}

interSimu() {
  read -p "server address : " server_address
  export SERVER_ADDRESS=$server_address
  read -p "bet count limit with 2 limit (ex 20,100): " bet_count_limit
  export BET_COUNT_LIMIT=$bet_count_limit
  read -p "robot count every room: " robot_count
  export ROBOT_COUNT=$robot_count
  printEnv
  export COMMD=" -e SERVER_ADDRESS=${SERVER_ADDRESS} \
  -e BET_COUNT_LIMIT=${BET_COUNT_LIMIT} \
  -e ROBOT_COUNT=${ROBOT_COUNT} \  "	
  runDocker
}

interBack(){
  read -p "mongo db host and port (ex 127.0.0.1:27017) : " mongo_host
  export MONGO_HOSTS=$mongo_host
  read -p "mongo db database : " mongo_database
  export MONGO_DATABASE=$mongo_database
  read -p "room name  : " room_name
  export ROOM_NAME=$room_name
  read -p "excel location : " excel_location
  export EXCEL_LOCATION=$excel_location
  read -p "output rows : " output_rows
  export OUTPUT_ROWS=$output_rows
  printEnv
  export COMMD="-e MONGO_HOSTS=${MONGO_HOSTS} \
  -e MONGO_DATABASE=${MONGO_DATABASE} \
  -e ROOM_NAME=${ROOM_NAME} \
  -e EXCEL_LOCATION=${EXCEL_LOCATION} \
  -e OUTPUT_ROWS=${OUTPUT_ROWS}  "	
  runDocker   
}

printEnv() {
	echo "echo all envs  start ------------------------------"
    echo "image name : $IMAGE_NAME"
    echo "scene : $SCENE"
  	echo "client num: $CLIENT_NUM"
    echo "download : $IF_DOWNLOAD"
    echo "download loc : $DOWNLOAD_LOCATION"
    echo "server_address: $SERVER_ADDRESS"
    echo "third_host: $THIRD_HOST"
    echo "third_port : $THIRD_PORT"
    echo "third_channel id : $THIRD_CHANNEL_ID"
    echo "rsa pub : $RSA_PUB"
    echo "rsa pri : $RSA_PRI"
    echo "port : $PORT"
    echo "bet count limit : $BET_COUNT_LIMIT"
    echo "mongo host : $MONGO_HOSTS"
    echo "mongo database : $MONGO_DATABASE"
  	echo "room name : $ROOM_NAME"
    echo "excel location : $EXCEL_LOCATION"
    echo "output rows : $OUTPUT_ROWS"
    echo "robot count : $ROBOT_COUNT"
	echo "echo all envs end ------------------------------"
}

build() {
  echo " delete old container start ......"
  sh delete.sh
  echo " delete old container done ......"
  echo " gradle build  start ......"
  gradle build
  echo " gradle build  end ......"
  echo " copy tar file to docker dir ......"
  cp build/distributions/houyi.tar docker/houyi.tar	
  cd docker
  echo " run docker build  start ......"
  docker build --rm -t houyi .
  echo " run docker build end ......" 
}

runDocker() {
 first="docker run -it --rm \
	--name houyicontainer \
	-e TZ=Asia/Shanghai \
	-e SCENE=${SCENE} "	
 last="${IMAGE_NAME}:latest"
 final="${first} ${COMMD} ${last}"
 echo $final
 eval $final
}

usage() {
	echo " "
	echo "Interaction mode : "
	echo "Pressure scenes: "
	echo "sh run.sh [IMAGE NAME] [SCENE] -interaction [SCENE DETAIL]"
	echo "example: sh run.sh houyi pressure -interaction guest"
	echo "Simulation and Backtest scenes: "
 	echo " "
	echo "sh run.sh [IMAGE NAME] [SCENE] -interaction"
	echo "example: sh run.sh houyi simulation -interaction"
	echo "----------------------------------------------- "
	echo "Quiet mode : "
	echo "Pressure scenes: "
	echo "sh run.sh [IMAGE NAME] pressure -quiet guest       [IF_DOWNLOAD][DOWNLOAD_LOCATION][CLIENT_NUM][SERVER_ADDRESS]"
	echo "sh run.sh [IMAGE NAME] pressure -quiet player      [IF_DOWNLOAD][DOWNLOAD_LOCATION][CLIENT_NUM][SERVER_ADDRESS]"
	echo "sh run.sh [IMAGE NAME] pressure -quiet third_party [IF_DOWNLOAD][DOWNLOAD_LOCATION][CLIENT_NUM][THIRD_HOST][THIRD_PORT][THIRD_CHANNEL_ID]"
	echo "example: sh run.sh houyi pressure -quiet guest false /opt/excel 100 http://127.0.0.1:8080/h5/qwe123 "
	echo "example: sh run.sh houyi pressure -quiet third_party false /opt/excel 100 http://127.0.0.1:8080 1 "
	echo " "
	echo "Simulation scenes: "
	echo "sh run.sh [IMAGE NAME] simulation -quiet           [SERVER_ADDRESS][BET_COUNT_LIMIT][ROBOT_COUNT]"
	echo "example: sh run.sh houyi simulation -quiet http://127.0.0.1:8080/h5/qwe123 50,2000(Lower limit and Upper limit) 5"
	echo " "
    echo "Backtest scenes: "
	echo "sh run.sh [IMAGE NAME] backtest -quiet             [MONGO_HOSTS][MONGO_DATABASE][ROOM_NAME][EXCEL_LOCATION][OUTPUT_ROWS]"
	echo "example: sh run.sh houyi backtest -quiet 192.168.50.13:27017 hgss B1 /opt 100"
}

if [ -z "$1" ]
then
	echo "Select one scene and one mode to continue this script"
	echo "There are three main scenes: simulation, pressure and backtest "
	echo "There are two mode: interaction and quiet "
	echo "First parameter is image name, second is scene, Third is mode "
	echo "Use sh run.sh -usage to get detail info"
fi

if [ "${2}" = "pressure" ] && [ "${3}" = "-quiet" ] ; then
   export IMAGE_NAME="$1"
   echo "Pressure quiet ${4}: "
   if [ "${4}" = "guest" ] ; then
       export SCENE="guest"
       quietGuestAndPlay "$5" "$6" "$7" "$8" "$9" "$10"
   elif [ "${4}" = "player" ] ; then
       export SCENE="player"
       quietGuestAndPlay "$5" "$6" "$7" "$8" "$9" "$10"
   elif [ "${4}" = "third_party" ] ; then
       export SCENE="third_party"
       quietThird "$5" "$6" "$7" "$8" "$9" "$10"
   fi    
elif [ "${2}" = "simulation" ] && [ "${3}" = "-quiet" ] ; then
   export IMAGE_NAME="$1"
   echo "Simulation quiet : "
   export SCENE="simulation"
   quietSimu "$4" "$5" "$6"
elif [ "${2}" = "backtest" ] && [ "${3}" = "-quiet" ] ; then
   export IMAGE_NAME="$1"
   export SCENE="backtest"
   echo "Backtest quiet : "
   quietBack "$4" "$5" "$6" "$7" "$8"    
elif [ "${2}" = "pressure" ] && [ "${3}" = "-interaction" ] ; then
   export IMAGE_NAME="$1"
   echo "Pressure interaction ${4}: "
   if [ "${4}" = "guest" ] ; then
      export SCENE="guest"
      interGuestAndPlay
   elif [ "${4}" = "player" ] ; then
      export SCENE="player"
      interGuestAndPlay
   elif [ "${4}" = "third_party" ] ; then
      export SCENE="third_party"
      interThird
   fi   
elif [ "${2}" = "simulation" ] && [ "${3}" = "-interaction" ] ; then
   export IMAGE_NAME="$1"
   export SCENE="simulation"
   echo "Simulation interaction : "
   interSimu    
elif [ "${2}" = "backtest" ] && [ "${3}" = "-interaction" ] ; then
   export IMAGE_NAME="$1"
   export SCENE="backtest"
   echo "backtest interaction : "
   interBack    
elif [ "${1}" = "-usage" ]; then
   usage
elif [ "${1}" = "-build" ]; then
   build   
fi