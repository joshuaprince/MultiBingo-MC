from django.db import models


class SetVariable(models.Model):
    space = models.ForeignKey('Space', db_index=True, on_delete=models.CASCADE)
    name = models.CharField(max_length=256)
    value = models.IntegerField()

    def __str__(self):
        return f"{self.space.goal_id} - {self.name}={self.value}"

    class Meta:
        unique_together = ['space', 'name']
