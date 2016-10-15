install --mode=700 --owner=webapp --group=webapp --directory /var/log/groovy-webapp-library-search
systemctl --no-reload preset groovy-webapp-library-search.service >/dev/null 2>&1
