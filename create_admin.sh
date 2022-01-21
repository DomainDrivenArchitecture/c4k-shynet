#!/bin/bash

python3 mysite/manage.py shell -c "from django.contrib.auth.models import User; User.objects.create_superuser('admin', 'a@a.com', 'test1234')"
