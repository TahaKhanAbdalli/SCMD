#!/bin/sh



echo "Done with upload-dgl-extension.sh"
if [ "$1" == "" ] ; then
    echo "ERROR! Missing JAR name to be deployed."
    echo "Usage: ./deploy-dgl-extension.sh <JAR_NAME>"
    exit 0
fi

JAR_NAME=$1

PREFIX=${JAR_NAME%-*}

echo "Starting deploy of DGL extension ($JAR_NAME)..."
echo ""

cd /tmp

echo "Cleaning previous versions..."
echo "mv /home/stibosw/$PREFIX* /home/stibosw/old_recipes"
echo ""

OLD_RECIPE_LIST=`sudo -u stibosw ls /home/stibosw/ | grep $PREFIX`
echo $OLD_RECIPE_LIST
for old_recipe in $OLD_RECIPE_LIST
  do
    sudo -u stibosw mv /home/stibosw/$old_recipe /home/stibosw/old_recipes
   done

echo "Copying jar to home folder..."
echo "sudo -u stibosw cp /tmp/$JAR_NAME /home/stibosw"
echo ""

sudo -u stibosw cp /tmp/$JAR_NAME /home/stibosw

echo ""
echo ""

VERSION_INDEX=${JAR_NAME%.*}
VERSION=${VERSION_INDEX##*-}

echo "Creating recipe for $PREFIX, version $VERSION..."
echo "sudo -u stibosw /opt/stibo/step/spot --package=/home/stibosw --target=$PREFIX,$VERSION --insecure=E9950549758E0E50"
echo ""

sudo -u shussain /opt/stibo/step/spot --package=/home/stibosw --target=$PREFIX,$VERSION --insecure=E9950549758E0E50

echo ""
echo ""

echo "Applying recipe..."
echo "sudo -u stibosw /opt/stibo/step/spot --apply=$PREFIX-$VERSION.spr --insecure=E9950549758E0E50"

sudo -u stibosw /opt/stibo/step/spot --apply=$PREFIX-$VERSION.spr --insecure=E9950549758E0E50

echo ""
echo ""

echo "deploy-dgl-extension.sh is done"
