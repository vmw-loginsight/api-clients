liauth() {
    command -v curl >/dev/null 2>/dev/null
    if [ $? != 0 ]; then echo "ERROR: curl command required"; fi

    if [ "$2" != "Local" -a "$2" != "ActiveDirectory" -o -z "$4" ]; then
        echo "USAGE: liauth <FQDN> <Local|ActiveDirectory> <USERNAME> <PASSWORD>"
    else
        LISESSIONID=$(curl -ski -X POST -H "content-type: application/json" \
-d '{ "provider": "'$2'", "username": "'$3'", "password": "'$4'" }' \
https://$1/api/v1/sessions)
        if [ ! -z "$(echo $LISESSIONID | grep -i sessionid)" ]; then
            LISESSIONID2=$(echo $LISESSIONID | awk '{split($0,a,"\""); print a[8];}')
        fi
        if [ ! -z "$LISESSIONID2" -a "$LISESSIONID2" != "FIELD_ERROR" ]; then
            export LISESSIONID=
            export LISESSIONID=$LISESSIONID2
            echo SUCCESS!
        else echo "$LISESSIONID"; fi
    fi
}

liapiusage() {
    echo 'USAGE: liget <FQDN> <GET|POST|PUT> <METHOD> [--pretty | <JSON> --pretty]
GET Methods
===========
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
vsphere|vsphere/:hostname|vsphere/:vspherehostname/hosts|vsphere/:hostname/hosts/esxihost>

POST Methods
============
appliance/vm-support-bundles
 {"target": "SINGLE", "manifests": {"Core:Logs"}}
deployment/approve
 {"workerAddress": "10.0.0.124", "workerPort": 16520, "workerToken": "85867eeb-3461-4dff-9294-e60f69467ab1", "approved": true}
deployment/join
 {"masterFQDN": "li.example.com"}
deployment/new
 {"user": {"userName": "admin", "password": "Password123!", "email": "admin@example.com"}}
events/ingest/<agentId>
 {"events": [{"text": "Hello world.", "timestamp": 123456789, "fields": [{"name": "field", "content": "value", "startPosition": 1, "length": 12, },...] },...] }
rollback
upgrades
 {"pakUrl": "http://example.com/sb/api/7580600/deliverable/?file=publish/VMware-vRealize-Log-Insight-3.7.0-7580600.UNSTABLE.TP.pak"}

PUT Methods
===========
upgrades/<version>/eula
 {"accepted": "true"}
'
}

liapi() {
    # Parameters
    if [ ! -z "$4" -a "$4" != "--pretty" ]; then DATA="-d $4"; fi

    # Checks
    command -v python >/dev/null 2>/dev/null
    if [ ! -z "$3" -a $? != 0 ]; then echo "ERROR: curl command required"
    elif [ -z "$3" ]; then liapiusage
    # Some API requests require authentication, otherwise do not
    elif [[ -z "$LISESSIONID" && \
        "$3" != deployment/join && \
        "$3" != deployment/new && \
        "$3" != events/ingest/* && \
        "$3" != version ]]; then echo "ERROR: Must authenticate first with liauth"
    # API -- must swallow errors given DATA variable requiring double quotations
    else
        echo curl -ski -X $2 -H 'Content-Type: application/json' -H 'Authorization: Bearer $(echo -n $LISESSIONID)' "$DATA" https://$1/api/v1/$3
        RESPONSE=$(curl -ski -X $2 -H 'Content-Type: application/json' -H 'Authorization: Bearer $(echo -n $LISESSIONID)' "$DATA" https://$1/api/v1/$3)
    fi

    if [ "$4" == "--pretty" -o "$5" == "--pretty" ]; then
        echo "$RESPONSE" | sed \$d
        echo "$RESPONSE" | tail -n 1 | python -m json.tool
    else echo "$RESPONSE"; fi
}

ncevent() {
    if [ -z "$3" ]; then echo "USAGE: ncevent <FQDN> <PORT> '<EVENT>'"
    else echo "$3" | nc -v -w 0 $1 $2; fi
}
