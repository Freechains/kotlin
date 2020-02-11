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

rm -Rf /tmp/8401
freechains host create /tmp/8401 8401
freechains host stop --host=localhost:8401
freechains host start /tmp/8401 &
sleep 0.5
freechains --host=localhost:8401 chain create /0
freechains --host=localhost:8400 chain put /0 text 111
freechains --host=localhost:8400 chain put /0 text 222
freechains --host=localhost:8400 chain send /0 localhost:8401
sleep 0.5

set -e
diff /tmp/8400/chains/0/ /tmp/8401/chains/0/
ret=`ls /tmp/8400/chains/0/ | wc`
if [ "$ret" != "      4       4     288" ]; then
  echo "$ret"
  exit 1
fi
set +e

echo
echo "=== ALL TESTS PASSED ==="
echo
