#!/bin/bash

set -x



ZOO=127.0.0.1:2181

PAXOS="$HOME/uringpaxos/target/build/Paxos-trunk/ringpaxos.sh"

node_a="localhost"
node_b="localhost"
node_c="localhost"
node_d="localhost"
node_e="localhost"
node_f="localhost"


xterm -geometry 120x20+0+0 -e ssh $node_a $PAXOS 11,1:P $ZOO &
sleep 0.2
xterm -geometry 120x20+0+300 -e ssh $node_b $PAXOS 11,2:A $ZOO &
sleep 0.2
xterm -geometry 120x20+0+600 -e ssh $node_c $PAXOS 11,3:A $ZOO &
sleep 0.2
xterm -geometry 120x20+900+0 -e ssh $node_d $PAXOS 11,4:A $ZOO &
sleep 0.2
xterm -geometry 120x20+900+300 -e ssh $node_e $PAXOS 11,5:L $ZOO &



wait
