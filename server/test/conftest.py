"""Кладите этот файл в server/test рядом с test_main.py.
Он добавляет папку server в пути поиска, чтобы тесты видели main.py и gemini_service.py."""
import os
import sys

SERVER_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if SERVER_DIR not in sys.path:
    sys.path.insert(0, SERVER_DIR)
