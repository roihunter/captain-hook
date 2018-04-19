# docker build --rm=true -t cpt-hook:latest .
# docker run --rm -it --publish=8005:8005 -v "/c/Users/m/Work/hook:/captain-hook" --name=cpt-hook cpt-hook:latest

FROM debian
MAINTAINER ROI Hunter

ENV TZ=Europe/Prague
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Install system dependencies
RUN apt-get update && apt-get install -y git python3 python3-pip

# Set the default directory
WORKDIR /app

# Install Python dependencies
ADD ./requirements.txt /app
RUN pip3 install -U pip wheel && pip3 install -r requirements.txt

# Add all files to container
ADD . /app

EXPOSE 8005

ENV GUNICORN_WORKERS=4
ENV GUNICORN_BIND="0.0.0.0:8005"

# Command to execute
CMD ["gunicorn", "--config=hooks/settings/settings_gunicorn.py", "hooks.app:app"]
