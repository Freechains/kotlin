#!/usr/bin/env sh

rm -Rf /tmp/8400
freechains host create /tmp/8400 8400
sed -i 's/"timestamp": true/"timestamp": false/g' /tmp/8400/host
freechains host stop --host=localhost:8400
freechains host start /tmp/8400 &
sleep 0.5
freechains --host=localhost:8400 chain create /0
h=`freechains --host=localhost:8400 chain put /0 text Hello_World`
freechains --host=localhost:8400 chain get /0 "$h" > /tmp/freechains-tests-get-1.out
freechains --host=localhost:8400 chain get /0 0_6e340b9cffb37a989ca544e6bb780a2c78901d3fb33738768511a30617afa01d > /tmp/freechains-tests-get-0.out

set -e
diff /tmp/freechains-tests-get-1.out  tests/freechains-tests-get-1.out

set +e

echo
echo "=== ALL TESTS PASSED ==="
echo
