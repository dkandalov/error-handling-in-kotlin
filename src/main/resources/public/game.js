function onPageLoad(gameId) {
    const es = new EventSource(`/game/events?gameId=${gameId}`);
    es.addEventListener('update', function(event) {
        const updatedGameId = event.data;
        if (updatedGameId === gameId) location.reload();
        else location.assign(`/game/${updatedGameId}`)
    });
}