FROM nightscape/docker-sbt
ENV conf conf/default.conf
RUN mkdir /app
ADD target/universal/loraley-1.0.zip /tmp/loraley.zip
RUN unzip /tmp/loraley.zip -d /app
CMD /app/loraley-1.0/bin/loraley ${conf}

