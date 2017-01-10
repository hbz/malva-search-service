getent group malva >/dev/null 2>&1 || groupadd malva
getent passwd malva >/dev/null 2>&1 || useradd -g malva -r malva
install --mode=770 --owner=malva --group=malva --directory /var/lib/malva-search-service
