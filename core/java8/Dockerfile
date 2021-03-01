# #
# # Licensed to the Apache Software Foundation (ASF) under one or more
# # contributor license agreements.  See the NOTICE file distributed with
# # this work for additional information regarding copyright ownership.
# # The ASF licenses this file to You under the Apache License, Version 2.0
# # (the "License"); you may not use this file except in compliance with
# # the License.  You may obtain a copy of the License at
# #
# #     http://www.apache.org/licenses/LICENSE-2.0
# #
# # Unless required by applicable law or agreed to in writing, software
# # distributed under the License is distributed on an "AS IS" BASIS,
# # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# # See the License for the specific language governing permissions and
# # limitations under the License.
# #

# FROM adoptopenjdk/openjdk11-openj9:x86_64-ubuntu-jdk-11.0.9_11_openj9-0.23.0

# RUN rm -rf /var/lib/apt/lists/* && apt-get clean && apt-get update \
# 	&& apt-get install -y --no-install-recommends locales \
# 	&& rm -rf /var/lib/apt/lists/* \
# 	&& locale-gen en_US.UTF-8

# ENV LANG="en_US.UTF-8" \
# 	LANGUAGE="en_US:en" \
# 	LC_ALL="en_US.UTF-8" \
# 	VERSION=8 \
# 	UPDATE=162 \
# 	BUILD=12

# ADD proxy /javaAction

# RUN cd /javaAction \
# 	&& rm -rf .classpath .gitignore .gradle .project .settings Dockerfile build \
# 	&& ./gradlew oneJar \
# 	&& rm -rf /javaAction/src \
# 	&& ./compileClassCache.sh

# # CMD ["java", "-Dfile.encoding=UTF-8", "-Xloggc:/tmp/container.gc", "-Xjit:verbose={compileStart|compileEnd|inlining},vlog=/tmp/container.jit", "-XcompilationThreads1", "-Xshareclasses:cacheDir=/javaSharedCache,readonly", "-Xquickstart", "-jar", "/javaAction/build/libs/javaAction-all.jar"]
# CMD ["java", "-Dfile.encoding=UTF-8", "-XcompilationThreads1", "-Xshareclasses:cacheDir=/javaSharedCache,readonly", "-Xquickstart", "-jar", "/javaAction/build/libs/javaAction-all.jar"]




# FROM ubuntu:18.04
FROM ghcr.io/graalvm/graalvm-ce:latest
ENV PORT 8080
ENV https_proxy=http://proxy.ethz.ch:3128
ENV http_proxy=http://proxy.ethz.ch:3128
ENV ftp_proxy=http://proxy.ethz.ch:3128
ENV no_proxy=localhost,127.0.0.1
ENV HTTPS_PROXY=http://proxy.ethz.ch:3128
ENV HTTP_PROXY=http://proxy.ethz.ch:3128
ENV FTP_PROXY=http://proxy.ethz.ch:3128
ENV NO_PROXY=localhost,127.0.0.1

ENV LANG="en_US.UTF-8" \
	LANGUAGE="en_US:en" \
	LC_ALL="en_US.UTF-8" \
	VERSION=8 \
	UPDATE=162 \
	BUILD=12

ADD proxy /javaAction

RUN cd /javaAction \
	&& rm -rf .classpath .gitignore .gradle .project .settings Dockerfile build \
	&& ./gradlew oneJar \
	&& rm -rf /javaAction/src 
	# && ./compileClassCache.sh
CMD ["java", "-Dfile.encoding=UTF-8","-Xloggc:/tmp/gc.log","-XX:+CITime","-XX:+PrintGCDetails","-XX:+PrintFlagsFinal","-verbose:gc","-XX:-UseCompressedOops", "-XX:+UseContainerSupport","-XX:MaxRAMPercentage=70","-XX:+UseSerialGC", "-jar", "/javaAction/build/libs/javaAction-all.jar"]

# # CMD ["java", "-Dfile.encoding=UTF-8", "-Xloggc:/tmp/container.gc", "-Xjit:verbose={compileStart|compileEnd|inlining},vlog=/tmp/container.jit", "-XcompilationThreads1", "-Xshareclasses:cacheDir=/javaSharedCache,readonly", "-Xquickstart", "-jar", "/javaAction/build/libs/javaAction-all.jar"]
# # CMD ["java", "-Xgcpolicy:optthruput","-Dfile.encoding=UTF-8", "-XX:+UseContainerSupport","-XX:MaxRAMPercentage=80","-XcompilationThreads1", "-Xshareclasses:cacheDir=/javaSharedCache,readonly", "-Xquickstart", "-jar", "/javaAction/build/libs/javaAction-all.jar"]

# CMD ["java", "-Dfile.encoding=UTF-8", "-Xloggc:tmp/gc.log","-XX:+UseContainerSupport","-XX:+UseSerialGC", "-jar", "/javaAction/build/libs/javaAction-all.jar"]


#RUN rm -rf /var/lib/apt/lists/* && apt-get clean && apt-get update \
#	&& apt-get install -y --no-install-recommends locales \
#	&& rm -rf /var/lib/apt/lists/* \
#	&& locale-gen en_US.UTF-8

#RUN  apt-get update \
#  && apt-get install -y wget \
#  && rm -rf /var/lib/apt/lists/*


#WORKDIR /home
#RUN wget -P /home/ https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.0.0/graalvm-ce-java11-linux-amd64-21.0.0.tar.gz \
#    && cd /home \
#    && tar xvf /home/graalvm-ce-java11-linux-amd64-21.0.0.tar.gz \
#    && cd - \
#    && rm /home/graalvm-ce-java11-linux-amd64-21.0.0.tar.gz

#ENV JAVA_HOME=/home/graalvm-ce-java11-21.0.0/
#ENV PATH="${JAVA_HOME}/bin:$PATH"