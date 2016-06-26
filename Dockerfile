########################################################
# Dockerfile to build Python application 'captain-hook'
# Based on Debain
########################################################

FROM debian
MAINTAINER ROI Hunter

# Install system dependencies
RUN apt-get update && apt-get install -y git python3 python3-pip supervisor

# Add all files to container, do not forget to create local config file
ADD . captain-hook/

# Set the default directory
WORKDIR /captain-hook

# Install Python dependencies
RUN pip3 install -U pip wheel && pip3 install --use-wheel -r requirements.txt

# Setup supervisor
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf
RUN mkdir -p /var/log/captain-hook

# Command to execute
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
