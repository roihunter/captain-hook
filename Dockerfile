# docker build --rm=true -t cpt-hook:latest .
# docker run --rm -it --publish=8005:8005 -v "/c/Users/m/Work/hook:/captain-hook" --name=cpt-hook cpt-hook:latest

FROM debian
MAINTAINER ROI Hunter

# Install system dependencies
RUN apt-get update && apt-get install -y git python3 python3-pip

# Add all files to container, do not forget to create local config file
ADD . captain-hook/

# Set the default directory
WORKDIR /captain-hook

# Install Python dependencies
RUN pip3 install -U pip wheel && pip3 install --use-wheel -r requirements.txt

# Command to execute
CMD ["gunicorn", "--workers=4", "--bind=0.0.0.0:8005", "hooks.app:app"]
