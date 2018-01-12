# -*- coding: utf-8 -*-

from os import environ


def get_env(variable, *, default=None):
    return environ.get("CAPTAINHOOK_" + variable.upper(), default)
