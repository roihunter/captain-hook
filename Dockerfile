########################################################
# Dockerfile to build Python application 'captain-hook'
# Based on Debain
########################################################

FROM debian
MAINTAINER ROI Hunter

# Install system dependencies
RUN apt-get update && apt-get install -y git python3 python3-pip supervisor

# Get GIT repository with project
RUN git clone -b master https://github.com/business-factory/captain-hook.git

# Set the default directory
WORKDIR /captain-hook

# Install Python dependencies
RUN pip3 install -U pip wheel && pip3 install --use-wheel -r requirements.txt

# Create local config file
ADD gold_digger/config/params_local.py gold_digger/config/params_local.py

# Setup supervisor
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
RUN mkdir -p /var/log/gold-digger

# Command to execute
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
