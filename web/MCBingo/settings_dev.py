# noinspection PyUnresolvedReferences
from .settings_base import *


# SECURITY WARNING: keep the secret key used in production secret!
SECRET_KEY = 'qn+e-fai7lli06x5#-4z^h$zqj^&lvcfq16#vzbuqf1g!v4g2!'  # Dev only - not for prod use

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = True

ALLOWED_HOSTS = ['localhost']


# Database
# https://docs.djangoproject.com/en/3.1/ref/settings/#databases

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
