# noinspection PyUnresolvedReferences
from .settings_base import *


SECRET_KEY = 'qn+e-fai7lli06x5#-4z^h$zqj^&lvcfq16#vzbuqf1g!v4g2!'  # Dev only - not for prod use

DEBUG = True

FRONTEND_HOST = 'localhost:3000'
ALLOWED_HOSTS = ['localhost']

INSTALLED_APPS = [
    'django.contrib.admin',
    'django.contrib.staticfiles',
] + INSTALLED_APPS

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': BASE_DIR / 'db.sqlite3',
    }
}


CHANNEL_LAYERS = {
    'default': {
        'BACKEND': 'channels.layers.InMemoryChannelLayer',
    },
}

STATIC_URL = '/static/'

LOG_DATABASE_ACCESS = True

LOGGING = {
    'version': 1,
    'filters': {
        'require_debug_true': {
            '()': 'django.utils.log.RequireDebugTrue',
        }
    },
    'handlers': {
        'console': {
            'level': 'DEBUG',
            'filters': ['require_debug_true'],
            'class': 'logging.StreamHandler',
        }
    },
    'loggers': {
        'django.db.backends': {
            'level': 'DEBUG',
            'handlers': ['console'] if LOG_DATABASE_ACCESS else [],
        }
    }
}
