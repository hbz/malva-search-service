getent group webapp >/dev/null 2>&1 || groupadd webapp
getent passwd webapp >/dev/null 2>&1 || useradd -g webapp -r webapp
install --mode=770 --owner=webapp --group=webapp --directory /var/lib/groovy-webapp-library-search
