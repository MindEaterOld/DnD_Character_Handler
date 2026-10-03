"""A tiny client for Android Studio's MCP Server plugin (streamable HTTP on 127.0.0.1:64342/stream).

The fallback for when the `android-studio` server from .mcp.json isn't loaded as tools.

Usage:
  python studio_mcp.py list                          tools and their parameters
  python studio_mcp.py schema <tool>                 one tool's full schema
  python studio_mcp.py call <tool> '<json args>'     call a tool
  python studio_mcp.py call <tool> @args.json        arguments from a file (no shell escaping)

Always pass "projectPath": "D:/Reps/DnD_Character_Handler" in the arguments.
"""
import json
import sys
import urllib.request

URL = 'http://127.0.0.1:64342/stream'
session = None
next_id = 0


def post(payload, timeout=900):
    global session
    headers = {'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream'}
    if session:
        headers['mcp-session-id'] = session
    request = urllib.request.Request(URL, data=json.dumps(payload).encode('utf-8'), headers=headers, method='POST')
    with urllib.request.urlopen(request, timeout=timeout) as response:
        session = response.headers.get('mcp-session-id') or session
        body = response.read().decode('utf-8')
        kind = response.headers.get('Content-Type', '')
    if not body.strip():
        return None
    if 'text/event-stream' in kind:
        messages = [json.loads(line[5:]) for line in body.splitlines() if line.startswith('data:') and line[5:].strip()]
        return messages[-1] if messages else None
    return json.loads(body)


def rpc(method, params=None, timeout=900):
    global next_id
    next_id += 1
    return post({'jsonrpc': '2.0', 'id': next_id, 'method': method, 'params': params or {}}, timeout)


def arguments(raw):
    if raw.startswith('@'):
        with open(raw[1:], encoding='utf-8') as source:
            return json.load(source)
    return json.loads(raw)


rpc('initialize', {'protocolVersion': '2025-03-26', 'capabilities': {}, 'clientInfo': {'name': 'ui-layout', 'version': '1'}})
post({'jsonrpc': '2.0', 'method': 'notifications/initialized'})

sys.stdout.reconfigure(encoding='utf-8')
if sys.argv[1] == 'list':
    for tool in rpc('tools/list')['result']['tools']:
        props = ', '.join(tool.get('inputSchema', {}).get('properties', {}).keys())
        print(f"{tool['name']}({props}): {tool.get('description', '').splitlines()[0][:150]}")
elif sys.argv[1] == 'schema':
    for tool in rpc('tools/list')['result']['tools']:
        if tool['name'] == sys.argv[2]:
            print(json.dumps(tool, indent=1, ensure_ascii=False))
else:
    result = rpc('tools/call', {'name': sys.argv[2], 'arguments': arguments(sys.argv[3]) if len(sys.argv) > 3 else {}})
    content = (result or {}).get('result', {}).get('content', [])
    print('\n'.join(part.get('text', '') for part in content) if content else json.dumps(result, ensure_ascii=False)[:20000])
