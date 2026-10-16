function startCall({ client, room, id, media, report, showRemote, ended }) {
  const connection = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] });
  const channel = client.channel(`call-${room}`, { config: { broadcast: { self: false }, presence: { key: id } } });
  const send = payload => channel.send({ type: 'broadcast', event: 'signal', payload: { from: id, ...payload } });
  const pending = [];
  let offered = false;

  const describe = async () => {
    await connection.setLocalDescription();
    await send({ type: connection.localDescription.type, sdp: connection.localDescription.sdp });
  };

  media.getTracks().forEach(track => connection.addTrack(track, media));
  connection.onicecandidate = ({ candidate }) => candidate && send({
    sdpMid: candidate.sdpMid, sdpMLineIndex: candidate.sdpMLineIndex, candidate: candidate.candidate,
  });
  connection.onconnectionstatechange = () => report(`state ${connection.connectionState}`);
  connection.ontrack = ({ track, streams }) => {
    report(`track ${track.kind}`);
    showRemote(streams[0]);
  };

  channel.on('broadcast', { event: 'signal' }, async ({ payload }) => {
    if (payload.sdp) {
      await connection.setRemoteDescription({ type: payload.type, sdp: payload.sdp });
      for (const candidate of pending.splice(0)) await connection.addIceCandidate(candidate);
      if (payload.type === 'offer') {
        offered = true;
        await describe();
      }
    } else if (payload.candidate) {
      const candidate = { candidate: payload.candidate, sdpMid: payload.sdpMid, sdpMLineIndex: payload.sdpMLineIndex };
      if (connection.remoteDescription) await connection.addIceCandidate(candidate);
      else pending.push(candidate);
    } else if (payload.type === 'bye') {
      report('bye');
      connection.close();
      ended();
    }
  });
  channel.on('presence', { event: 'sync' }, async () => {
    for (const peer of Object.keys(channel.presenceState())) {
      if (!offered && id < peer) {
        offered = true;
        report('offering');
        await describe();
      }
    }
  });
  channel.subscribe(status => {
    report(`channel ${status}`);
    if (status === 'SUBSCRIBED') channel.track({ joined: 'yes' });
  });

  const hangUp = async () => {
    await send({ type: 'bye' });
    connection.close();
    await client.removeChannel(channel);
  };
  return { connection, hangUp };
}
