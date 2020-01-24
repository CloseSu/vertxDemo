#!/bin/sh
ip="127.0.0.1"
apiPort="9000"
udpPort=5140
url="http://$ip:$apiPort/api"
headers="content-type:application/json"
tokenDir="/usr/share/graylog/data/config/token.txt"

while ! nc -vz 127.0.0.1 9000; do
  echo "wait for connection"
  sleep 1
done

if [ -e token ]
then
	echo "api processd and exit this script"
    exit
else
    echo "process api.................................."
fi


#get token
echo "token: $token ==============================================="
token=$(\
curl -i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' \
"$url/system/sessions" \
 -d '{"username":"admin", "password":"admin123456", "host":""}' \
|grep -o "\{.*\}" \
| jq ".session_id" \
| sed -r "s/\\\"//g"
)
mytoken="$token:session"
echo "mytoken: $mytoken ==============================================="
echo $token > token

#input
inputId=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/system/inputs" \
-d '{ "title": "Input", "type": "org.graylog2.inputs.gelf.udp.GELFUDPInput", "global": "true", "configuration": {"recv_buffer_size": 262144,"bind_address": "0.0.0.0",  "port": 5140, "decompress_size_limit": 8388608 }, "node": "null"}'\
|grep -o '\{.*\}' \
| jq '.id' \
| sed -r "s/\\\"//g"
)

echo "inputId: $inputId ==============================================="

#collectors
collectorsId=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/plugins/org.graylog.plugins.collector/configurations?createDefaults=true" \
-d '{"name": "nginx",  "tags": ["nginx"],"inputs": [],  "outputs": [], "snippets": [] }' \
|grep -o '\{.*\}' \
| jq '.id' \
| sed -r "s/\\\"//g"
)

echo "collectorsId: $collectorsId ==============================================="

#Collectors outputs
outputId=$(\
curl -u "$mytoken" \
-i -X GET -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/plugins/org.graylog.plugins.collector/configurations/$collectorsId" \
|grep -o '\{.*\}' \
| jq '.outputs[0].output_id' \
| sed -r "s/\\\"//g"
)

echo "outputId: $outputId ==============================================="

#Collectors inputs
collectorsInputs=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/plugins/org.graylog.plugins.collector/configurations/$collectorsId/inputs" \
-d '{"backend": "nxlog","type": "file","name": "nginx-in", "forward_to": "$outputId","properties": {"poll_interval": "1","save_position": "true","read_last": "true", "recursive": "true","rename_check": "false", "multiline": "false", "multiline_start": "/^-./","path": "/var/log/nginx/access.log"} }' \
)

echo "collectorsInputs: $collectorsInputs ==============================================="


#configuredExtractors
extractorId=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/system/inputs/$inputId/extractors" \
-d '{"title": "nginx","cut_or_copy": "copy","source_field": "message","target_field": "",	"extractor_type": "json","extractor_config": {"list_separator": ", ","key_separator": "_","kv_separator": ":","key_prefix": "","key_whitespace_replacement": "_"},	"converters": {},"condition_type": "none",	"condition_value": ""}'\
|grep -o '\{.*\}' \
| jq '."extractor_id"' \
| sed -r "s/\\\"//g"
)

echo "extractorId: $extractorId ==============================================="

#Index id
indexId=$(\
curl -u "$mytoken" \
-i -X GET -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/system/indices/index_sets?skip=0&limit=0&stats=false" \
|grep -o '\{.*\}' \
| jq '.index_sets[0].id' \
| sed -r "s/\\\"//g"
)

echo "indexId: $indexId ==============================================="

#setDefaultIndexSet not use in 3.0+

#Dashboards
dashboardId=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/dashboards" \
-d '{"title": "Performance Monitor", "description": "Performance Monitor" }'\
|grep -o '\{.*\}' \
| jq '."dashboard_id"' \
| sed -r "s/\\\"//g"
)

echo "dashboardId: $dashboardId ==============================================="

#regex grok WebSocketResponseURL
WebSocketResponseURL=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/system/grok" \
-d '{"name": "WebSocketResponseURL","pattern": "request\\s.*\\sused\\s\\d{1,}\\sms"}'\
)

echo "WebSocketResponseURL: $WebSocketResponseURL ==============================================="

#regex grok HttpResponseURL
HttpResponseURL=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/system/grok" \
-d '{"name": "HttpResponseURL","pattern": "used\\s\\d{1,}\\sms\\suri\\s*+"}' \
)

echo "HttpResponseURL: $HttpResponseURL ==============================================="

#addHttpExtractor
addHttpExtractor=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/system/inputs/$inputId/extractors" \
-d  '{ "title": "HttpResponseURL",  "cut_or_copy": "copy", "source_field": "full_message", "target_field": "", "extractor_type": "grok", "extractor_config": { "grok_pattern": "%{HttpResponseURL}" },"converters": {}, "condition_type": "regex",  "condition_value": "\\[Performance\\]\\[HTTP\\].*used\\s\\d{1,}\\sms.*"}' \
)

echo "addHttpExtractor: $addHttpExtractor ==============================================="


#addWebSocketExtractor
addWebSocketExtractor=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/system/inputs/$inputId/extractors" \
-d  '{"title": "WebSocketResponseURL", "cut_or_copy": "copy","source_field": "full_message","target_field": "", "extractor_type": "grok", "extractor_config": { "grok_pattern": "%{WebSocketResponseURL}" }, "converters": {}, "condition_type": "regex", "condition_value": "\\[Performance\\]\\[WS\\].*used\\s\\d{1,}\\sms.*" }' \
)

echo "addWebSocketExtractor: $addWebSocketExtractor ==============================================="

#Create Quick Values Widgets
widgetsId01=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/dashboards/$dashboardId/widgets" \
-d '{"description": "Http Response URL", "type": "QUICKVALUES", "cache_time": 3000,"config": { "timerange": { "type": "relative", "range": 0 }, "field": "HttpResponseURL", "show_pie_chart": "true",  "query": "", "show_data_table": "true" } }' \
|grep -o '\{.*\}' \
| jq '."widget_id"' \
| sed -r "s/\\\"//g"
)

widgetsId02=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/dashboards/$dashboardId/widgets" \
-d '{"description": "WebSocket Response URL", "type": "QUICKVALUES", "cache_time": 3000,"config": { "timerange": { "type": "relative", "range": 0 }, "field": "WebSocketResponseURL", "show_pie_chart": "true",  "query": "", "show_data_table": "true" } }' \
|grep -o '\{.*\}' \
| jq '."widget_id"' \
| sed -r "s/\\\"//g"
)

widgetsId03=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/dashboards/$dashboardId/widgets" \
-d '{"description": "UA", "type": "QUICKVALUES", "cache_time": 3000,"config": { "timerange": { "type": "relative", "range": 0 }, "field": "ua", "show_pie_chart": "true",  "query": "", "show_data_table": "true" } }' \
|grep -o '\{.*\}' \
| jq '."widget_id"' \
| sed -r "s/\\\"//g"
)

widgetsId04=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/dashboards/$dashboardId/widgets" \
-d '{"description": "Response Code", "type": "QUICKVALUES", "cache_time": 3000,"config": { "timerange": { "type": "relative", "range": 0 }, "field": "status", "show_pie_chart": "true",  "query": "", "show_data_table": "true" } }' \
|grep -o '\{.*\}' \
| jq '."widget_id"' \
| sed -r "s/\\\"//g"
)

widgetsId05=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/dashboards/$dashboardId/widgets" \
-d'{"description":"VisitIP","type":"QUICKVALUES","cache_time":3000,"config":{"timerange":{"type":"relative","range":0},"field":"client","show_pie_chart":"true","query":"","show_data_table":"true"}}'\
|grep -o '\{.*\}' \
| jq '."widget_id"' \
| sed -r "s/\\\"//g"
)

widgetsId06=$(\
curl -u "$mytoken" \
-i -X POST -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/dashboards/$dashboardId/widgets" \
-d '{"description": "API Respose Time", "type": "QUICKVALUES", "cache_time": 3000,"config": { "timerange": { "type": "relative", "range": 0 }, "field": "url", "show_pie_chart": "true",  "query": "", "show_data_table": "true" } }' \
|grep -o '\{.*\}' \
| jq '."widget_id"' \
| sed -r "s/\\\"//g"
)

echo "widgetsId01: $widgetsId01 ==============================================="
echo "widgetsId02: $widgetsId02 ==============================================="
echo "widgetsId03: $widgetsId03 ==============================================="
echo "widgetsId04: $widgetsId04 ==============================================="
echo "widgetsId05: $widgetsId05 ==============================================="
echo "widgetsId06: $widgetsId06 ==============================================="


#widgetPositions
widgetPositions=$(\
curl -u "$mytoken" \
-i -X PUT -H "$headers" \
-H 'Accept: application/json' \
-H 'X-Requested-By: cli' "$url/dashboards/$dashboardId/positions" \
-d  '{"positions":[{"id":"\"$widgetsId01\"","col":1,"row":1,"height":3,"width":1},{"id":"\"$widgetsId02\"","col":2,"row":1,"height":3,"width":1},{"id":"\"$widgetsId03\"","col":3,"row":1,"height":3,"width":1},{"id":"\"$widgetsId04\"","col":4,"row":1,"height":3,"width":1},{"id":"\"$widgetsId05\"","col":5,"row":1,"height":3,"width":1},{"id":"\"$widgetsId06\"","col":1,"row":2,"height":3,"width":1}]}' \
)
echo "widgetPositions: $widgetPositions ==============================================="

echo "done====================================="

