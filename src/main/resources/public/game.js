function onPageLoad() {
    const es = new EventSource('/game/events?gameId={{gameId}}');
    es.addEventListener('update', function(event) {
        const gameId = event.data;
        if (gameId === '{{gameId}}') location.reload();
        else location.assign(`/game/events?gameId=${gameId}`)
    });
}