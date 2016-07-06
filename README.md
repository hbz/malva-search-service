# Library services implemented as a Groovy web application platform

This web application provides a platform for libraries microservices
by using Elasticsearch as a HTTP server and back end. It is based
on the Web application plugin for Elasticsearch at 

`sru` - Search/Retrieve API for Elasticsearch

`lp` - routing application for inter library copy request system in Germany,
serving requests for scans from printed material as well as from electronic resources
at publisher sites

`fl` - online resource converter application to create rasterized graphics from
 original source to allow libraries in inter library networks to adhere to UrhG §53a

There are also an FTP client, PDF doument builder, and an SMTP mailer implemented
to support the web application.

More to come soon.

## Versions

| Elasticsearch version | Web app version  | Release date |
| --------------------- | ---------------- | -------------|
| 2.1.1                 | 2.1.1.0          | Jan  2, 2016 |
| 1.5.2                 | 1.5.2.9-SNAPSHOT | Dec 31, 2015 |

## Configuration

You have to add configuration files

```
src/main/webapp/config.json
src/integration-test/resources/test-config.yml
```

by yourself to this project. They are not in the repository because they
contain private data.

## Documentation

TODO

# License

elasticsearch-webapp-libraryservice - livrary services as a web application platform based on Elasticsearch

Copyright (C) 2016 Jörg Prante and the xbib organization

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.