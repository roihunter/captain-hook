# -*- coding: utf-8 -*-

from os import environ, path

from ._settings_default import *

PROFILE = environ.get("CAPTAINHOOK_PROFILE", "local")

if PROFILE == "master":
    from ._settings_master import *
elif PROFILE == "staging":
    from ._settings_staging import *
elif PROFILE == "local":
    try:
        from ._settings_local import *
    except ImportError:
        raise LookupError(
            "Local configuration not found. Create file _settings_local.py in {} directory according to README.".format(
                path.abspath(path.join(__file__, path.pardir))
            )
        )
else:
    raise ValueError("Unsupported settings profile. Use one of {}. Given: '{}'.".format(("master", "staging", "local"), PROFILE))
