#! /bin/bash
set -x
source /tmp/parameter

#####################################################################################
#                          Check for root user                                      #
#####################################################################################

if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root"
  exit 1
fi

#####################################################################################
#        Basic Initialization/Settings needed for installation                      #
#####################################################################################

VHOME=""
echo "Creating the Vault installation directory..."

mkdir -p /opt/tvault

if [[ -d /opt/tvault ]]; then
	export VHOME="/opt/tvault"
else
	echo "Unable to create $VHOME. Installation failed."
	exit 1
fi

if [[ ! $(getent passwd tvault) ]] ; then
   echo "Creating the user tvault..."
   useradd tvault || true
fi
if [[ ! $(getent group tvault) ]]; then
   echo "Creating the group tvault..."
   groupadd tvault || true
   usermod -g tvault tvault
fi
#useradd tvault || true
#groupadd tvault || true
#usermod -g tvault tvault

VLOG="/var/log/app/"
chown -R tvault:tvault /var/log/tvault
mkdir -p $VLOG  || { echo "$VLOG folder creation failed" ; exit 1; }
echo "Creating the directory [$VLOG] to write application logs..."

mkdir /var/log/tvault
chown -R tvault:tvault /var/log/tvault
chmod -R  ugo+r $VLOG
VDOWNLOADS="$VHOME/tmp"
mkdir -p "$VDOWNLOADS"

INSTLOG="$VLOG/tvault-install.log"
echo "The installation logs will be available at [$INSTLOG]..."

##############################################################################
#       Copy the required Binaries and grant permissions                     #
##############################################################################

echo "Untaring $VDOWNLOADS/$VINST and installing Tvault in $VHOME..."
tar -xf /tmp/vault.tar.gz -C "$VHOME"
chown -R tvault:tvault $VHOME
chown -R tvault:tvault $VLOG


##############################################################################
#                               Utils/Helper Functions                       #
##############################################################################

# Heler to validate IP Address

function valid_ip()
{
  local  ip=$1
  local  stat=1

  if [[ $ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
    OIFS=$IFS
    IFS='.'
    ip=($ip)
    IFS=$OIFS
    [[ ${ip[0]} -le 255 && ${ip[1]} -le 255 \
    && ${ip[2]} -le 255 && ${ip[3]} -le 255 ]]
    stat=$?
  fi
  return $stat
}

# Helper to generate keys
function genselfcert()
{
  if ! type "openssl" > /dev/null; then
    return 1
  fi

  IP=$(hostname -I | cut -d' ' -f1)

  if valid_ip $IP; then
    echo "IP.1 = $IP" >> $2
  else
    echo "Unable to determine IP address. Exiting ..."
  exit 1
  fi

  cert_pass=$3
  openssl req -x509 -batch -nodes -newkey rsa:2048 -keyout $1/tvault.key -out $1/tvault.crt -config $2 -days 9999
  openssl pkcs12 -export -in $1/tvault.crt -inkey $1/tvault.key -out $1/tvault.p12 -name self -passout pass:$cert_pass
}
SSCRED_FILE_LOCATION="/opt/tvault/hcorp"
##############################################################################
# End - Utils
##############################################################################

##############################################################################
#                    Generate Certificates...                                #
##############################################################################

_use_selfsigned=$SELF_SIGNED
CERT_PASSWORD=""

if [[ "$_use_selfsigned" == "n" ]]; then
  echo " => Copy the tvault.crt, tvault.key and tvault.p12 file to $VHOME/certs"
  echo -n " => Enter the password for the PKCS12 keystore:"; read CERT_PASSWORD;

  if [[ ! -f $VHOME/certs/tvault.crt ||  ! -f $VHOME/certs/tvault.key  || ! -f $VHOME/certs/keystore.p12 ]]; then
    echo "Certificate file not found in $VHOME/certs. Exiting ..."
    exit 1
  else
  echo "Certificate files found."
  fi

else
  echo "Generating self signed certificates..."
  CERT_PASSWORD=$(head /dev/urandom | tr -dc A-Za-z0-9 | head -c 14)
  genselfcert $VHOME/certs $VHOME/certs/template.cfr $CERT_PASSWORD
  chown -R tvault:tvault $VHOME/certs
  export VAULT_SKIP_VERIFY=1
  echo "VAULT_SKIP_VERIFY=1" >> $VCONF
fi

ELB_ADD=$ELB_VALUE
if [ -n "$_redirect_addr" ]; then
   export VAULT_REDIRECT_ADDR=$_redirect_addr
fi
echo "The Vault Redirect address is [$VAULT_REDIRECT_ADDR]..."
echo "VAULT_REDIRECT_ADDR=$VAULT_REDIRECT_ADDR" >> $VCONF

##############################################################################
# End - Collect installation parameters
##############################################################################

################################################################################
# API start
################################################################################

echo "Setting up web application"

API_CONF="$VHOME/api/bin/tvaultapi.conf"
touch API_CONF
echo "JAVA_OPTS=\"-DTVAULT-API-LOG-PATH=$VLOG/\"" >> $API_CONF
echo "LOG_FOLDER=$VLOG" >> $API_CONF
echo "RUN_ARGS=\"--vault.api.url=https://13.235.238.21:8200/v1 --selfservice.ssfilelocation=$SSCRED_FILE_LOCATION --vault.port=8200 --vault.auth.method=$AUTH_BACKEND --vault.ssl.verify=false --server.port=8443 --server.ssl.key-store=/opt/tvault/certs/tvault.p12 --server.ssl.keyStoreType=PKCS12 --server.ssl.key-store-password=$CERT_PASSWORD\"" >> $API_CONF

chmod +x $VHOME/api/bin/tvaultapi.jar
chmod +x $VHOME/web/nginx/sbin/nginx
chmod +x $VHOME/web/bin/tnginx

ln -sf $VHOME/web/bin/tnginx /etc/init.d/tnginx
ln -sf $VHOME/api/bin/tvaultapi.jar /etc/init.d/tvaultapi

echo "Starting api service ... "
systemctl enable tvaultapi.service
service tvaultapi start

################################################################################
# End API start
###############################################################################

################################################################################
# Removing Vault Init
################################################################################

FILE="/opt/tvault/hcorp/vault.init"

################################################################################
# Startup - Adding services on startup
################################################################################

chkconfig tvault on
chkconfig tvaultapi on
chkconfig tnginx on

################################################################################
# Nginx start
################################################################################

echo "Starting web server ... "
systemctl enable tnginx.service
service tnginx start

################################################################################
# End - Nginx start
################################################################################
