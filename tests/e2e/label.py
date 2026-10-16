import sys
import xml.etree.ElementTree as ET

nodes = ET.fromstring(sys.stdin.read()).iter('node')
texts = [n.get('text') for n in nodes if n.get('class') == 'android.widget.TextView' and n.get('text')]
print(max(texts, key=len) if texts else '')
