# -*- coding: utf-8 -*-
"""
Related article: https://sebest.github.io/post/protips-using-gunicorn-inside-a-docker-image/

Parameters you might want to override:
  GUNICORN_BIND="0.0.0.0:8005"
"""

import os


workers = 4
bind = "0.0.0.0:8005"
worker_class = "eventlet"
worker_connections = 10

# Overwrite some Gunicorns params by ENV variables
for k, v in os.environ.items():
    if k.startswith("GUNICORN_"):
        key = k.split('_', 1)[1].lower()
        locals()[key] = v
