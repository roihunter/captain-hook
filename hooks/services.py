# -*- coding: utf-8 -*-

import graypy
import logging

from . import settings
from .publisher import RabbitPublisher


def logger():
    logger = logging.getLogger("hooks")
    logger.setLevel(logging.DEBUG if settings.DEVELOPMENT_MODE else logging.INFO)

    for handler in logging.root.handlers:
        handler.addFilter(logging.Filter("hooks"))

    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter(
        "[%(levelname)s] %(asctime)s at %(filename)s:%(lineno)d (%(processName)s) -- %(message)s",
        "%Y-%m-%d %H:%M:%S")
    )
    logger.addHandler(handler)

    if not settings.DEVELOPMENT_MODE:
        handler = graypy.GELFHandler(settings.graylog["host"], settings.graylog["port"])
        logger.addHandler(handler)

    return logger


def publisher():
    return RabbitPublisher(**settings.messaging)
