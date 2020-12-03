# Generated by Django 3.1.3 on 2020-11-29 07:33

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('backend', '0001_initial'),
    ]

    operations = [
        migrations.CreateModel(
            name='PlayerBoard',
            fields=[
                ('id', models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID')),
                ('player_name', models.CharField(max_length=5)),
                ('squares', models.CharField(max_length=26)),
                ('board', models.ForeignKey(on_delete=django.db.models.deletion.CASCADE, to='backend.board')),
            ],
        ),
    ]