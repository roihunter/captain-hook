# docker build --rm=true -t cpt-hook:latest .
# docker run --rm -it --publish=8005:8005 --name=cpt-hook cpt-hook:latest

FROM python:3.6
MAINTAINER ROI Hunter

ENV TZ=Europe/Prague
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

ADD ./requirements.txt /app

RUN pip install -U pip wheel && pip install -r requirements.txt

ADD . /app

EXPOSE 8005

ENV GUNICORN_WORKERS=4
ENV GUNICORN_BIND="0.0.0.0:8005"

CMD ["gunicorn", "--config=hooks/settings/settings_gunicorn.py", "hooks.app:app"]
