package pl.edu.agh.ha.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.ha.db.Item;
import pl.edu.agh.ha.db.ItemRepo;

@RestController
public class ApiEndpoint {
    @Autowired
    private ItemRepo itemRepo;

    @RequestMapping(value = "/api/item", method = RequestMethod.POST)
    public void add(@RequestBody ItemDto itemDto) {
        Item item = new Item();
        item.setName(itemDto.getName());
        item.setDescription(itemDto.getDescription());
        itemRepo.save(item);
    }

    @RequestMapping(value = "/api/item", method = RequestMethod.GET)
    public Iterable<Item> getAll() {
        return itemRepo.findAll();
    }

    @RequestMapping(value = "/api/item/{id}", method = RequestMethod.GET)
    public Item get(@PathVariable("id") Long id) {
        return itemRepo.findOne(id);
    }

    @RequestMapping(value = "/api/item/{id}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("id") Long id) {
        itemRepo.delete(id);
    }

    @RequestMapping(value = "/api/item", method = RequestMethod.DELETE)
    public void deleteAll() {
        itemRepo.deleteAll();
    }
}
