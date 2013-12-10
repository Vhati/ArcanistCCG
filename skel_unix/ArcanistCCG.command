#!/bin/bash
HERE=$(dirname "$0");
cd "${HERE}";

java -cp "ArcanistCCG.jar" org.arcanist.client.ArcanistCCG
