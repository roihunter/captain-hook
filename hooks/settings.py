# -*- coding: utf-8 -*-

messaging = {
    "login": "guest",
    "password": "guest",
    "host": "localhost",
    "port": 5672,
    "virtual_host": "hooks",

    "exchange_name": "events",
    "queue_master": "events.master",
    "queue_staging": "events.staging",
    "queue_testing": "events.testing",
}

graylog = {
    "host": None,
    "port": None,
}

DEVELOPMENT_MODE = True
FACEBOOK_VERIFY_TOKEN = "token"


try:
    from hooks.settings_local import *
except ImportError:
    raise Exception("You are missing local settings in file 'settings_local.py'")
