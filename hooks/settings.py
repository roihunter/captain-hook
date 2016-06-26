# -*- coding: utf-8 -*-

DEVELOPMENT_MODE = True
FACEBOOK_VERIFY_TOKEN = "token"

RABBIT_LOGIN = "guest"
RABBIT_PASSWORD = "guest"
RABBIT_HOST = "localhost"
RABBIT_PORT = 5672
RABBIT_VIRTUAL_HOST = "hooks"
RABBIT_EXCHANGE = "events"

GRAYLOG_HOST = None
GRAYLOG_PORT = None


try:
    from hooks.settings_local import *
except ImportError:
    raise Exception("You are missing local settings in file 'settings_local.py'")
