from backend.models.color import Color

# List of marking colors that count towards the win detector
WINNING_MARKINGS = [
    Color.COMPLETE,
    Color.NOT_INVALIDATED,
]
