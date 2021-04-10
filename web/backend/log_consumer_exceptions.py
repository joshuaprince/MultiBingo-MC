"""
Independent module to enable exception logging for Django Channels async consumers, since
it tends to hide any errors raised within the consumer.

Usage: Decorate any Async Websocket Consumer classes with @log_consumer_exceptions

Source: https://stackoverflow.com/a/58849175/11972329
Issue in Channels Github: https://github.com/django/channels/issues/1498
"""


import sys
import traceback

from functools import wraps
from inspect import iscoroutinefunction

from channels.exceptions import AcceptConnection, DenyConnection, StopConsumer
from django.conf import settings


def _log_exceptions(f):
    @wraps(f)
    async def wrapper(*args, **kwargs):
        try:
            return await f(*args, **kwargs)
        except (AcceptConnection, DenyConnection, StopConsumer):
            raise
        except Exception as exception:
            if not getattr(exception, "logged_by_wrapper", False):
                print(
                    "Unhandled exception occurred in {}:".format(f.__qualname__),
                    file=sys.stderr,
                )
                traceback.print_exc(file=sys.stderr)
                setattr(exception, "logged_by_wrapper", True)
            raise

    return wrapper


def log_consumer_exceptions(klass):
    if settings.DEBUG:
        for method_name, method in list(klass.__dict__.items()):
            if iscoroutinefunction(method):
                setattr(klass, method_name, _log_exceptions(method))

    return klass
