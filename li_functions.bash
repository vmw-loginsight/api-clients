liauth() {
    command -v curl >/dev/null 2>/dev/null
    if [ $? != 0 ]; then echo "ERROR: curl command required"; fi
    if [ "$2" != "Local" -a "$2" != "ActiveDirectory" -o -z "$4" ]
    then
        echo "USAGE: liauth <FQDN> <Local|ActiveDirectory> <USERNAME> <PASSWORD>"
    else
        LISESSIONID=$(curl -s -X POST -i -k \
            -H "content-type: application/json" \
            -d '{ "provider": "'$2'", "username": "'$3'", "password": "'$4'" }' \
            https://$1/api/v1/sessions)
        if [ ! -z "$(echo $LISESSIONID | grep -i sessionid)" ]; then
            export LISESSIONID=$(echo $LISESSIONID | awk '{split($0,a,"\""); print a[8];}')
        else
            echo "ERROR: sessionID not found

$LISESSIONID"
        fi
    fi
}

liget() {
    command -v python >/dev/null 2>/dev/null
    if [ ! -z "$3" -a $? != 0 ]; then echo "ERROR: curl command required"; fi
    if [ -z "$2" ]; then echo "USAGE: liget <FQDN> <METHOD> [--pretty]
Methods:
ad/config
adgroups|adgroups/<domain>/<name>|adgroups/<domain>/<name>/[capabilities|groups|datasets]|
agent|agent/groups|
appliance/support-bundles/<id>?timeout=<timeout>|
archiving/config|
cluster/nodes|cluster/nodes/:<nodetoken>|
datasets|datasets/<dataSetsID>|
forwarding|
health/resources/:<clusternodeid>|health/activequeries|health/statistics|
groups|groups/<groupId>|groups/<groupId>/[users|adgroups|capabilities|datasets]|
ilb|ilb/status|
licenses|
notifications|
time|time/config|
upgrades/:version|upgrades/:version/nodes/:node|
users|users/<userId>|users/<userId>/[capabilities|groups|datasets]|
version|
vrops|vrops/:exampleHostname|
vsphere|vsphere/:hostname|vsphere/:vspherehostname/hosts|vsphere/:hostname/hosts/esxihost>"
    elif [ -z "$LISESSIONID" ]; then echo "ERROR: Must authenticate first with liauth"
    elif [ ! -z "$3" ]; then curl -sk -X GET -H 'Content-Type: application/json' \
        -H 'Authorization: Bearer '$(echo -n $LISESSIONID) https://$1/api/v1/$2 \
        | python -m json.tool
    else curl -sk -X GET -H 'Content-Type: application/json' \
        -H 'Authorization: Bearer '$(echo -n $LISESSIONID) https://$1/api/v1/$2
    fi
}
